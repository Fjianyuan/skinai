from threading import Lock

import cv2
import numpy as np


def select_prediction_mode(mode, face_path, lesion_path):
    """Resolve the configured mode without silently ignoring a partial model pair."""
    if mode == 'yolo':
        return mode
    if mode not in ('auto', 'unet'):
        raise ValueError('PREDICTION_MODE must be auto, unet, or yolo.')

    face_exists = face_path.is_file()
    lesion_exists = lesion_path.is_file()
    if face_exists and lesion_exists:
        return 'unet'
    if mode == 'auto' and not face_exists and not lesion_exists:
        return 'yolo'
    raise ValueError('Two-stage segmentation requires both U-Net ONNX files.')


class BinaryUNet:
    """Run an ONNX U-Net with RGB input and one foreground-logit channel."""

    def __init__(self, model_path, input_size):
        if input_size <= 0:
            raise ValueError('U-Net input size must be positive.')
        self.input_size = input_size
        self.network = cv2.dnn.readNetFromONNX(str(model_path))
        self.lock = Lock()

    def __call__(self, image):
        blob = cv2.dnn.blobFromImage(
            image, scalefactor=1 / 255, size=(self.input_size, self.input_size), swapRB=True,
        )
        with self.lock:
            self.network.setInput(blob)
            logits = self.network.forward().copy()
        if logits.ndim != 4 or logits.shape[:2] != (1, 1):
            raise ValueError('U-Net must return one NCHW foreground-logit channel.')

        # A positive logit corresponds to a foreground probability above 0.5.
        restored = cv2.resize(logits[0, 0], image.shape[:2][::-1], interpolation=cv2.INTER_LINEAR)
        return restored > 0


def segment_lesions(image, face_model, lesion_model):
    """Return lesion contours in original-image pixels, or None for an empty face mask."""
    face = face_model(image).astype(np.uint8)
    if not face.any():
        return None

    x, y, width, height = cv2.boundingRect(face)
    roi = np.s_[y:y + height, x:x + width]
    face_crop = face[roi]
    masked_crop = cv2.bitwise_and(image[roi], image[roi], mask=face_crop)
    lesions = lesion_model(masked_crop).astype(np.uint8) & face_crop
    contours, _ = cv2.findContours(
        lesions, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE, offset=(x, y),
    )
    return list(contours)
