# highavailable
HA framework for Java applications, supported loadbalance.

To start test:
  1. 'java -DLB_CONFIG_DIR="<LB_CONFIG_DIR>" -cp "<CLASSPATH>" suit.max.highavailable.test.LoadBalancerTest' to run the loadbalancer test.
  2. 'java -DLB_CONFIG_DIR="<LB_CONFIG_DIR>" -cp "<CLASSPATH>" suit.max.highavailable.test.SlaveTest' to run the slave test.
  3. 'java -cp "<CLASSPATH>" suit.max.highavailable.test.CallerClientTest <loadbalancer ip address>' to run the test client.

if it works right, you will get "s.m.h.test.SyncEventHandlerTest - TEST MESSAGE : Hi Server!" in your slave log after you run the test client.
