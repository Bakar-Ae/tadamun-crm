# Frontend Security Header Test Results

## Test

Command:

```powershell
Invoke-WebRequest -UseBasicParsing -Uri "http://localhost:5173" -Method Get | Select-Object -ExpandProperty Headers