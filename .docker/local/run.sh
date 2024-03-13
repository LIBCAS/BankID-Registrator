#!/bin/bash

# Print environment variables to the log
echo "Logging environment variables..."
env

dos2unix mvnw
./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"