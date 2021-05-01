$rio = 'rio'  # this is IP address of roborio
$logdir = "D:\Util\logs"  # update this path to the directory where you placed badlogvis.exe
$rioFound = Test-Connection $rio -Count 1 -TTL 2 -Quiet
if ($rioFound) {
    Write-Host -ForegroundColor Cyan "Copying logs from roboRio"
    & scp -p $rio`:/home/lvuser/*.bag "$($logdir)\"
    & scp -p $rio`:/media/sda1/*.bag "$($logdir)\"
    Write-Host -ForegroundColor Yellow "Removing logs from roboRio"
    Start-Process -FilePath ssh -ArgumentList "$rio rm *.bag" -Wait -WindowStyle Hidden  #remove files from rio to prevent disk space issues
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
