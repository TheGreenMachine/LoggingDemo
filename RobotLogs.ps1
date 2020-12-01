$rio = '10.18.16.2'  # this is IP address of roborio
$logdir = "D:\Util\BadLog"  # update this path to the directory where you placed badlogvis.exe

Write-Host "Copying logs from roboRio"
& scp -p $rio`:/home/lvuser/*.bag "$($logdir)\"
Write-Host "Removing logs from roboRio"
Start-Process -FilePath ssh -ArgumentList "$rio rm *.bag" -Wait -WindowStyle Hidden  #remove files from rio to prevent disk space issues
Write-Host "Creating html files"
Get-ChildItem $logdir\*.bag | ForEach-Object { & "$logdir\badlogvis.exe" $_ }
Remove-Item $logdir\*.bag *> $null
& $(Get-ChildItem $logdir\*.html | Sort-Object LastWriteTime | Select-Object -last 1)
