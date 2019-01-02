#Requires -Version 2

Param (
  [String] $Connection,
  [String[]] $Objects,
  [String[]] $Folders,
  [Int] $RuleSet,
  [String] $ReportName,
  [String] $ReportFolder,
  [Switch] $HTML,
  [Switch] $JSON,
  [Switch] $XLS,
  [Switch] $XML,
  # Fail conditions
  [Int] $Halstead,
  [Int] $Maintainability,
  [Int] $McCabe,
  [Int] $TCR,
  [Switch] $RuleViolations,
  [Switch] $SyntaxErrors,
  [Switch] $IgnoreWrappedPackages
)

# Decode Oracle connection string
$DecodedConnection = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Connection))

# Decode database objects formatted as "type.owner.name"
$DecodedObjects = @()
if ($Objects) {
  ForEach($Object in $Objects) {
    $Type, $Owner, $Name = $Object.Split('.')

    $DecodedType = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Type))
    $DecodedOwner = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Owner))
    $DecodedName = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Name))

    $DecodedObject = New-Object PSObject
    $DecodedObject | Add-Member NoteProperty Type $DecodedType
    $DecodedObject | Add-Member NoteProperty Owner $DecodedOwner
    $DecodedObject | Add-Member NoteProperty Name $DecodedName

    $DecodedObjects += $DecodedObject
  }
}

# Decode database object folders
$DecodedFolders = @()
if ($Folders) {
  ForEach($Folder in $Folders) {
    $Path, $Filter, $Recurse = $Folder.Split('.')

    $DecodedPath = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Path))
    $DecodedFilter = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Filter))
    $DecodedRecurse = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Recurse))

    # Convert from string to boolean
    $DecodedRecurse = [System.Convert]::ToBoolean($DecodedRecurse)

    $DecodedFolder = New-Object PSObject
    $DecodedFolder | Add-Member NoteProperty Path $DecodedPath
    $DecodedFolder | Add-Member NoteProperty Filter $DecodedFilter
    $DecodedFolder | Add-Member NoteProperty Recurse $DecodedRecurse

    $DecodedFolders += $DecodedFolder
  }
}

# Decode report arguments
$DecodedReportName = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($ReportName))
$DecodedReportFolder = [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($ReportFolder))

# Start TDT
$TDT = New-Object -ComObject 'Toad.ToadAutoObject'
try {

  # Create the output path if it doesn't exist
  if ($DecodedReportFolder) {
    New-Item -itemType Directory -force -path $DecodedReportFolder | Out-Null
  }

  # Set CA parameters
  $TDT.CodeAnalysis.Connection = $TDT.Connections.NewConnection($DecodedConnection)

  $TDT.CodeAnalysis.RuleSet = $RuleSet

  $TDT.CodeAnalysis.ReportName = $DecodedReportName
  $TDT.CodeAnalysis.OutputFolder = $DecodedReportFolder
  $TDT.CodeAnalysis.ReportFormats.IncludeHTML = $HTML
  $TDT.CodeAnalysis.ReportFormats.IncludeJSON = $JSON
  $TDT.CodeAnalysis.ReportFormats.IncludeXLS = $XLS
  $TDT.CodeAnalysis.ReportFormats.IncludeXML = $XML

  Write-Output "Setting fail conditions..."

  $TDT.CodeAnalysis.FailConditions.CheckHalstead = $Halstead -gt 0
  if ($TDT.CodeAnalysis.FailConditions.CheckHalstead) {
    $TDT.CodeAnalysis.FailConditions.HalsteadLevel = $Halstead - 1

    $Output = "Fail condition... Halstead: {0}"
    switch ($TDT.CodeAnalysis.FailConditions.HalsteadLevel) {
      0 { $Output = $Output -f 'Reasonable (< 1000)' }
      1 { $Output = $Output -f 'Challenging (1001 - 3000)' }
      2 { $Output = $Output -f 'Too Complex (> 3000)' }
    }
    Write-Output $Output
  }

  $TDT.CodeAnalysis.FailConditions.CheckMaintainability = $Maintainability -gt 0
  if ($TDT.CodeAnalysis.FailConditions.CheckMaintainability) {
    $TDT.CodeAnalysis.FailConditions.MaintainabilityLevel = $Maintainability - 1

    $Output = "Fail condition... Maintainability: {0}"
    switch ($TDT.CodeAnalysis.FailConditions.MaintainabilityLevel) {
      0 { $Output = $Output -f 'Highly maintainable (> 85)' }
      1 { $Output = $Output -f 'Moderate maintainability (65 - 85)' }
      2 { $Output = $Output -f 'Difficult to maintain (< 65)' }
    }
    Write-Output $Output
  }

  $TDT.CodeAnalysis.FailConditions.CheckMcCabe = $McCabe -gt 0
  if ($TDT.CodeAnalysis.FailConditions.CheckMcCabe) {
    $TDT.CodeAnalysis.FailConditions.McCabeLevel = $McCabe - 1

    $Output = "Fail condition... McCabes: {0}"
    switch ($TDT.CodeAnalysis.FailConditions.McCabeLevel) {
      0 { $Output = $Output -f 'Simple program, small risk (< 11)' }
      1 { $Output = $Output -f 'More complex program, moderate risk (11 - 20)' }
      2 { $Output = $Output -f 'Very complex program, high risk (21 - 50)' }
      3 { $Output = $Output -f 'Untestable program, very high risk (> 50)' }
    }
    Write-Output $Output
  }

  $TDT.CodeAnalysis.FailConditions.CheckTCR = $TCR -gt 0
  if ($TDT.CodeAnalysis.FailConditions.CheckTCR) {
    $TDT.CodeAnalysis.FailConditions.TCRLevel = $TCR - 1

    $Output = "Fail condition... Toad Code Rating: {0}"
    switch ($TDT.CodeAnalysis.FailConditions.TCRLevel) {
      0 { $Output = $Output -f 'Good (< 2)' }
      1 { $Output = $Output -f 'OK (2)' }
      2 { $Output = $Output -f 'Fair (3)' }
      3 { $Output = $Output -f 'Poor (> 3)' }
    }
    Write-Output $Output
  }

  $TDT.CodeAnalysis.FailConditions.CheckRuleViolations = $RuleViolations
  if ($TDT.CodeAnalysis.FailConditions.CheckRuleViolations) {
    Write-Output 'Fail condition... Rule violations'
  }
  $TDT.CodeAnalysis.FailConditions.CheckSyntaxErrors = $SyntaxErrors
  if ($TDT.CodeAnalysis.FailConditions.CheckSyntaxErrors) {
    Write-Output 'Fail condition... Syntax errors'
  }
  $TDT.CodeAnalysis.FailConditions.IgnoreWrappedPackages = $IgnoreWrappedPackages
  if ($TDT.CodeAnalysis.FailConditions.IgnoreWrappedPackages) {
    Write-Output 'Fail condition... Ignore wrapped packages'
  }

  # Set files to analyze
  ForEach($DecodedFolder in $DecodedFolders) {
    if ($DecodedFolder.Recurse) {
      Get-ChildItem $DecodedFolder.Path -filter $DecodedFolder.Filter -recurse | % {
        if (!$_.PSIsContainer) {
          Write-Output "Preparing to analyze... $_"
          $TDT.CodeAnalysis.Files.Add($_.FullName)
        }
      }
    } else {
      Get-ChildItem $DecodedFolder.Path -filter $DecodedFolder.Filter | % {
        if (!$_.PSIsContainer) {
          Write-Output "Preparing to analyze... $_"
          $TDT.CodeAnalysis.Files.Add($_.FullName)
        }
      }
    }
  }

  # Set database object info to analyze
  ForEach($DecodedObject in $DecodedObjects) {
    $Output = "{0}.{1} {2}" -f $DecodedObject.Owner, $DecodedObject.Name, $DecodedObject.Type
    Write-Output "Preparing to analyze... $Output"

    $DBObject = $TDT.CodeAnalysis.DBObjects.Add()
    $DBObject.ObjectName  = $DecodedObject.Name
    $DBObject.ObjectOwner = $DecodedObject.Owner
    $DBObject.ObjectType  = $DecodedObject.Type
  }

  # Execute Code Analysis
  $TDT.CodeAnalysis.Execute()

  # Report failures to the caller
  if ($TDT.CodeAnalysis.Errors.Count -gt 0) {
    Write-Output 'Code analysis contained one or more errors...'
    Write-Output $TDT.CodeAnalysis.Errors.ToString()
    # This will be interpreted by the caller to fail the build step
    Write-Output 'FAILURE'
  }

  # Toad DevOps Toolkit 1.2+ includes a results property which contains rule violation details
  if (Get-Member -InputObject $TDT.CodeAnalysis -Name "Results") {
    # Report rule violations to the caller
    if ($TDT.CodeAnalysis.FailConditions.CheckRuleViolations -and $TDT.CodeAnalysis.Results.RuleViolations.Count -gt 0) {
      Write-Output 'Code analysis contained one or more rule violations...'
      Write-Output $TDT.CodeAnalysis.Results.RuleViolations.ToString()
      # This will be interpreted by the caller to fail the build step
      Write-Output 'FAILURE'
    }
  }

} catch {
  Write-Output $_.Exception.Message
  Write-Output 'FAILURE'
} finally {
  $TDT.Quit()
}
