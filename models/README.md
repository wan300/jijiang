# MIS captcha model

This directory stores the BJTU MIS captcha model as a root-repository artifact.
The backend code currently loads the model from the classpath resource path below
and that runtime path is intentionally unchanged:

```text
12group-backend/src/main/resources/bjtu_captcha_crnn.pt
```

Before starting the backend locally, building the backend JAR, or building the
backend Docker image, copy the model from this directory to that path.

PowerShell:

```powershell
Copy-Item -Force .\models\bjtu_captcha_crnn.pt .\12group-backend\src\main\resources\bjtu_captcha_crnn.pt
```

Bash:

```bash
cp -f ./models/bjtu_captcha_crnn.pt ./12group-backend/src/main/resources/bjtu_captcha_crnn.pt
```

Expected SHA-256:

```text
7059482D85836FA9FA64CAE359226519B98B6B1CBD931F6E420C010074EF15AA
```
