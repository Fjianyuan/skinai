import io
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import cv2
import numpy as np
from flask import Flask

from services.unet_segmentation import BinaryUNet, segment_lesions, select_prediction_mode


class UNetSegmentationTests(unittest.TestCase):
    def test_second_model_receives_masked_crop_and_contours_use_original_coordinates(self):
        image = np.full((30, 40, 3), 200, dtype=np.uint8)
        face = np.zeros(image.shape[:2], dtype=bool)
        face[5:25, 10:30] = True
        face[5:10, 10:15] = False

        def lesions(crop):
            self.assertEqual(crop.shape, (20, 20, 3))
            self.assertTrue((crop[0, 0] == 0).all())
            self.assertTrue((crop[10, 10] == 200).all())
            mask = np.zeros(crop.shape[:2], dtype=bool)
            mask[:4, :4] = True
            mask[7:12, 8:13] = True
            return mask

        contours = segment_lesions(image, lambda _: face, lesions)
        self.assertEqual(len(contours), 1)
        self.assertEqual(cv2.boundingRect(contours[0]), (18, 12, 5, 5))

    def test_empty_face_mask_skips_lesion_model(self):
        image = np.zeros((30, 40, 3), dtype=np.uint8)

        def lesions(_):
            self.fail('The second model should not run without a face.')

        self.assertIsNone(segment_lesions(image, lambda _: image[:, :, 0].astype(bool), lesions))

    def test_face_without_lesions_produces_an_empty_list(self):
        image = np.zeros((12, 18, 3), dtype=np.uint8)
        self.assertEqual(segment_lesions(
            image,
            lambda img: np.ones(img.shape[:2], dtype=bool),
            lambda crop: np.zeros(crop.shape[:2], dtype=bool),
        ), [])

    def test_model_adapter_uses_rgb_and_restores_mask_to_original_size(self):
        class Network:
            def setInput(self, blob):
                self.blob = blob

            def forward(self):
                return np.array([[[[-4, 4], [-4, 4]]]], dtype=np.float32)

        network = Network()
        image = np.zeros((10, 20, 3), dtype=np.uint8)
        image[:, :, 2] = 255
        with patch('cv2.dnn.readNetFromONNX', return_value=network):
            model = BinaryUNet(Path('fixture.onnx'), 16)
            mask = model(image)
        np.testing.assert_array_equal(network.blob[0, :, 0, 0], [1, 0, 0])
        self.assertEqual(network.blob.shape, (1, 3, 16, 16))
        self.assertEqual(mask.shape, image.shape[:2])
        self.assertFalse(mask[:, :10].any())
        self.assertTrue(mask[:, 10:].all())

    def test_auto_preserves_yolo_and_rejects_an_incomplete_pair(self):
        with tempfile.TemporaryDirectory() as directory:
            face, lesions = (Path(directory) / name for name in ('face.onnx', 'lesions.onnx'))
            self.assertEqual(select_prediction_mode('auto', face, lesions), 'yolo')
            face.touch()
            with self.assertRaises(ValueError):
                select_prediction_mode('auto', face, lesions)
            lesions.touch()
            self.assertEqual(select_prediction_mode('auto', face, lesions), 'unet')
            self.assertEqual(select_prediction_mode('yolo', face, lesions), 'yolo')

    def test_prediction_endpoint_returns_contours_without_guessing_lesion_types(self):
        from api import predict

        app = Flask(__name__)
        app.register_blueprint(predict.predict_api)
        image = np.zeros((30, 40, 3), dtype=np.uint8)
        contour = np.array([[[7, 6]], [[7, 10]], [[13, 10]], [[13, 6]]], dtype=np.int32)
        with tempfile.TemporaryDirectory() as directory:
            app.config.update(UPLOAD_FOLDER=directory, RESULT_FOLDER=directory)
            encoded = cv2.imencode('.png', image)[1].tobytes()
            with (
                patch.object(predict, 'load_prediction_models', return_value=('unet', (None, None))),
                patch.object(predict, 'segment_lesions', return_value=[contour]),
                patch.object(predict, 'get_deepseek_diagnosis', return_value={}),
            ):
                response = app.test_client().post('/api/predict/', data={
                    'file': (io.BytesIO(encoded), 'face.png'),
                })
            self.assertEqual(response.status_code, 200)
            data = response.get_json()['data']
            self.assertEqual(data['acne_count'], 1)
            self.assertEqual(data['acne_types'], [])
            self.assertEqual(data['lesion_contours'], [[[7, 6], [7, 10], [13, 10], [13, 6]]])
            self.assertTrue(data['image_base64'].startswith('data:image/jpeg;base64,'))


if __name__ == '__main__':
    unittest.main()
