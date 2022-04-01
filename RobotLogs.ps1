<#
.SYNOPSIS
Gets Logs from robot

.DESCRIPTION
Gets logs from roborio in lvusr home or usb storage or from simulator

.PARAMETER rio
ssh address of rio use ip address or host

.PARAMETER logdir
directory that holds your output logs and badlogvis.exe

.PARAMETER last
how many of the most recent files to retrieve

#>
function Get-RobotLogs( $rio = 'rio', $logdir = "D:\Util\logs", $last = 1 ) {
    $rioFound = Test-Connection $rio -Count 1 -TTL 2 -Quiet
    if ($rioFound) {
        Write-Host -ForegroundColor Cyan "Copying most recent from roboRio"
        $recent = & ssh -fq $rio ls -rt /home/lvuser/*.bag | tail -$($last)
        if($recent) {
            & scp -p $rio`:$recent "$($logdir)\"
            Write-Host -ForegroundColor Yellow "Removing logs from roboRio"
            & ssh -fq $rio rm *.bag #remove files from rio to prevent disk space issues
            & ssh -fq $rio rm /media/sda1/*.bag #remove files from rio to prevent disk space issues
        }
    }
    Write-Host -ForegroundColor Cyan "Copying simulation logs"
    Get-ChildItem $env:TEMP\*.bag | Sort-Object -Descending -Property LastWriteTime -Top $last | Move-Item -Destination $logdir -Force
    $bagFiles = Get-ChildItem $logdir\*.bag
    if ($bagFiles) {
        Write-Host -ForegroundColor Cyan "Creating html files"
        $bagFiles | ForEach-Object { & "$logdir\badlogvis.exe" $_ }
        Remove-Item $logdir\*.bag *> $null
        & $(Get-ChildItem $logdir\*.html | Sort-Object -Descending -Property LastWriteTime -Top 1)
    }
    else {
        Write-Host -ForegroundColor Cyan "No new bag files"
    }
}

Get-RobotLogs
