# Download Maven wrapper JAR
$url = "https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.2.0/maven-wrapper-3.2.0.jar"
$output = ".mvn/wrapper/maven-wrapper.jar"

# Create directory if it doesn't exist
New-Item -ItemType Directory -Force -Path ".mvn/wrapper" | Out-Null

Write-Host "Downloading Maven wrapper..."
Invoke-WebRequest -Uri $url -OutFile $output
Write-Host "Maven wrapper downloaded successfully!"
