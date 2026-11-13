# SkinAI 项目说明

SkinAI 是学生团队开发的面部痤疮检测原型。Android 客户端将测试图像发给 Flask 后端，接收标注图和检测数量。应用也支持查看检测记录和每日打卡。

## 研究原型与个人分工

付建元担任项目队长，提出先用 U-Net 分割面部区域，再用 YOLOv8 在该区域内定位痤疮，最后用 U-Net 勾画病灶轮廓。团队把推理流程接入 Flask 后端并完成 Android 端部署。

## 公开代码

当前仓库的 [Flask 推理接口](flask/backend/api/predict.py) 直接对上传图像运行 YOLOv8，并用 OpenCV 绘制检测结果。配置 DeepSeek 后，后端可以根据检测摘要和用户选填的文字描述生成文本。检测记录和打卡数据由 SQL Server 保存。

公开仓库包含 Android 客户端和 Flask 后端。复现研究原型还需要训练数据、模型权重和 U-Net 训练流程。运行公开版本所需的配置见 [README](README.md)。
