
#!/bin/bash
# Run server module

mvn -pl tuttifrutti-server -am exec:java -Dexec.mainClass="com.tf.server.Server"


