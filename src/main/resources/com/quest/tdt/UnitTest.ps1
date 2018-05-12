#Requires -Version 2

Param (
  [String] $Connection,
  [String[]] $Objects,
  [String] $OutputPath,
  [Switch] $TXT,
  [Switch] $XML
)

# Used for removing any invalid file name characters
Function Remove-InvalidFileNameChars {
  Param (
    [Parameter(Mandatory = $True,
      Position = 0,
      ValueFromPipeline = $True,
      ValueFromPipelineByPropertyName = $True)]
    [String] $Name
  )

  $InvalidChars = [IO.Path]::GetInvalidFileNameChars() -join ''
  $RegEx = "[{0}]" -f [RegEx]::Escape($InvalidChars)
  Return $Name -replace $RegEx
}

# Decode Oracle connection string
$DecodedConnection = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Connection))

# Decode database objects formatted as "owner.name"
$DecodedObjects = @()
ForEach($Object in $Objects) {
  $Owner, $Name = $Object.Split('.')

  $DecodedOwner = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Owner))
  $DecodedName = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Name))

  $DecodedObject = New-Object PSObject
  $DecodedObject | Add-Member NoteProperty Owner $DecodedOwner
  $DecodedObject | Add-Member NoteProperty Name $DecodedName

  $DecodedObjects += $DecodedObject
}

# Decode output path
$DecodedOutputPath = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($OutputPath))

# Start TDT
$TDT = New-Object -ComObject 'Toad.ToadAutoObject'
try {
  $TDT.UnitTesting.Connection = $TDT.Connections.NewConnection($DecodedConnection)

  # Set database objects
  ForEach($DecodedObject in $DecodedObjects) {
    $DBObject = $TDT.UnitTesting.UnitTests.DBObjects.Add()
    $DBObject.ObjectName  = $DecodedObject.Name
    $DBObject.ObjectOwner = $DecodedObject.Owner
  }

  # Get the units tests for the object(s)
  $TDT.UnitTesting.UnitTests.Refresh()

  # Create the output path if it doesn't exist
  if ($DecodedOutputPath) {
    New-Item -itemType Directory -force -path $DecodedOutputPath | Out-Null
  }

  # Execute unit tests
  ForEach($UnitTest in $TDT.UnitTesting.UnitTests) {
    $UnitTest.Execute() | Out-Null

    Write-Output ("Running... {0} Status: {1}" -f $UnitTest.Name, $UnitTest.LastRunStatus)

    # Report failures to the caller
    if ($UnitTest.LastRunStatus -ne 'SUCCESS') {
      # This will be interpreted by the caller to fail the build step
      Write-Output 'FAILURE'
    }

    if ($TXT) {
      $OutputFile = "{0} - {1}.txt" -f $UnitTest.Name, $UnitTest.LastRunStatus
      $OutputFile = Remove-InvalidFileNameChars $OutputFile
      $OutputFile = Join-Path $DecodedOutputPath $OutputFile
      $UnitTest.GetLastRunReportText() | Out-File $OutputFile
    }

    if ($XML) {
      $OutputFile = "{0} - {1}.xml" -f $UnitTest.Name, $UnitTest.LastRunStatus
      $OutputFile = Remove-InvalidFileNameChars $OutputFile
      $OutputFile = Join-Path $DecodedOutputPath $OutputFile
      $UnitTest.GetLastRunReportXML() | Out-File $OutputFile
    }
  }
} catch {
  Write-Output $_.Exception.Message
  Write-Output 'FAILURE'
} finally {
  $TDT.Quit()
}
