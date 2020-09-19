#!/bin/zsh

# did you change the version number?
sbt clean
sbt assembly
sbt docker:publishLocal
docker image tag iotsocketbridge:latest intel-server-03:5000/iotsocketbridge
docker image push intel-server-03:5000/iotsocketbridge

# Server side:
# kubectl apply -f /home/appuser/deployments/iotSocketBridge.yaml
# kubectl expose deployment iot-socket-bridge --type=LoadBalancer --port 8889
# If needed:
# kubectl delete deployment iot-socket-bridge
# For troubleshooting
# kubectl exec --stdin --tty iot-socket-bridge -- /bin/bash