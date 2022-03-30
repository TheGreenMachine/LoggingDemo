$rio = 'rio'  # this is IP address of roborio
$logdir = "D:\Util\logs"  # update this path to the directory where you placed badlogvis.exe
$rioFound = Test-Connection $rio -Count 1 -TTL 2 -Quiet

if ($rioFound) {
    Write-Host -ForegroundColor Cyan "Copying most recent from roboRio"
    Start-Process -FilePath ssh -ArgumentList "ls -rt /home/lvuser/*.bag | tail -1" -Wait -WindowStyle Hidden -RedirectStandardOutput "$($logdir)\recent.log"
    $recent = Get-Content "$($logdir)\recent.log"
    & scp -p $rio`:/home/lvuser/$recent "$($logdir)\"
    & scp -p $rio`:/media/sda1/$recent "$($logdir)\"
    Write-Host -ForegroundColor Yellow "Removing logs from roboRio"
    Start-Process -FilePath ssh -ArgumentList "$rio rm *.bag" -Wait -WindowStyle Hidden  #remove files from rio to prevent disk space issues
    Start-Process -FilePath ssh -ArgumentList "$rio rm /media/sda1/*.bag" -Wait -WindowStyle Hidden  #remove files from rio to prevent disk space issues
}
Write-Host -ForegroundColor Cyan "Copying simulation logs"
Move-Item $env:TEMP\*.bag $logdir -Force
$bagFiles = Get-ChildItem $logdir\*.bag
if ($bagFiles) {
    Write-Host -ForegroundColor Cyan "Creating html files"
    $bagFiles | ForEach-Object { & "$logdir\badlogvis.exe" $_ }
    Remove-Item $logdir\*.bag *> $null
    & $(Get-ChildItem $logdir\*.html | Sort-Object LastWriteTime | Select-Object -last 1)
} else {
    Write-Host -ForegroundColor Cyan "No new bag files"
}
