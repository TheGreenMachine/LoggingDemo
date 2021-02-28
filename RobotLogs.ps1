$rio = 'rio'  # this is IP address of roborio
$logdir = "D:\Util\BadLog"  # update this path to the directory where you placed badlogvis.exe

Write-Host "Copying logs from roboRio"
& scp -p $rio`:/home/lvuser/*.bag "$($logdir)\"
& scp -p $rio`:/media/sda1/*.bag "$($logdir)\"
Write-Host "Removing logs from roboRio"
# remove files from rio to prevent disk space issues
Start-Process -FilePath ssh -ArgumentList "$rio rm /home/lvuser/*.bag" -Wait -WindowStyle Hidden
Start-Process -FilePath ssh -ArgumentList "$rio rm /media/sda1/*.bag" -Wait -WindowStyle Hidden
Write-Host "Creating html files"
Get-ChildItem $logdir\*.bag | ForEach-Object { & "$logdir\badlogvis.exe" $_ }
Remove-Item $logdir\*.bag *> $null
& $(Get-ChildItem $logdir\*.html | Sort-Object LastWriteTime | Select-Object -last 1)
