# iotSocketBridge
This is a simple, straightforward bridge for iot devices to use instead of publishing to a Kafka topic. 
There are ways to code that connection directly but this was simpler to implement. The iot device simply
opens a TCP socket to this socket listener and the code will forward that string, untouched, to the provided 
Kafka topic. 

I also have a UDP version of the same functionality.  