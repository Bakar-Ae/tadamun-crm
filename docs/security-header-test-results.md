# Security Header Test Results

## Test

Command:

```powershell
Invoke-WebRequest -Uri "http://localhost:8081/actuator/health" -Method Get | Select-Object -ExpandProperty Headers