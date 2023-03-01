#Requires -PSEdition Core

<#
.SYNOPSIS
Gets Logs from robot

.DESCRIPTION
Gets logs from roborio in lvusr home or usb storage or from simulator

.PARAMETER rio
ssh address of rio use ip address or host. Change 'rio' to your ip

.PARAMETER logdir
directory that holds your output logs and badlogvis.exe. Change "D:\Util\logs" to you path where badlogvis is located.

.PARAMETER last
how many of the most recent files to retrieve

#>
function GetRobotLogs( $rio = '10.18.16.2', $logdir = "C:\Users\nicho\Badlog", $last = 1 ) {
    $rioFound = Test-Connection $rio -Count 1 -TTL 2 -Quiet
    Write-Host -ForegroundColor Cyan "Checking for connection to host: $rio"
    if ($rioFound) {
        Write-Host -ForegroundColor Cyan "Copying most recent from roboRio to $logdir"
        $recent = & ssh -fq $rio ls -rt /home/lvuser/*.bag | Select-Object -Last $($last)
        if($recent) {
            & scp -p $rio`:$recent "$($logdir)\"
        }
    }
    Write-Host -ForegroundColor Cyan "Copying simulation logs to $logdir"
    Get-ChildItem $env:TEMP\*.bag | Sort-Object -Descending -Property LastWriteTime -Top $last | Move-Item -Destination $logdir -Force
    $bagFiles = Get-ChildItem $logdir\*.bag
    if ($bagFiles) {
        Write-Host -ForegroundColor Cyan "Creating html files in $logdir"
        $bagFiles | ForEach-Object { & "$logdir\badlogvis.exe" $_ }
        Remove-Item $logdir\*.bag *> $null
        & $(Get-ChildItem $logdir\*.html | Sort-Object -Descending -Property LastWriteTime -Top 1)
    }
    else {
        Write-Host -ForegroundColor Cyan "No new bag files"
    }
}

# Do not execute if being used as a include
if ("" -eq $MyInvocation.ScriptName) {
    GetRobotLogs
}
