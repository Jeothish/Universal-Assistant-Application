# 3rd-Year-Project

Whisper:
    pip install openai-whisper
    pip install torch torchvision torchaudio --index-url https://download.pytorch.org/whl/cpu
  
  Also needed to run whisper -> FFMPEG
  
  download FFMPEG (through chocolatey) by running these commands in powershell as admin:
  
  Chocolatey download:
    Set-ExecutionPolicy Bypass -Scope Process -Force; `
    [System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072; `
    iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
    
  FFMPEG download (powershell as admin):
    choco install ffmpeg
    
  OR
    
  download FFMPEG from https://ffmpeg.org/download.html, without choco.
  
  Set FFMPEG path if not already set by choco:
    C:\ProgramData\chocolatey\lib\ffmpeg\tools\ffmpeg\bin
