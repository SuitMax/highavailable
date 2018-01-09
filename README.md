# highavailable
HA framework for Java applications, supported loadbalance.

To start test:
  1. 'java -DLB_CONFIG_DIR="<LB_CONFIG_DIR>" -cp "<CLASSPATH>" suit.max.highavailable.demo.LoadBalancerDemo' to run the loadbalancer demo.
  2. 'java -DLB_CONFIG_DIR="<LB_CONFIG_DIR>" -cp "<CLASSPATH>" suit.max.highavailable.demo.SlaveDemo' to run the slave demo.
  3. 'java -cp "<CLASSPATH>" suit.max.highavailable.demo.CallerClientDemo <loadbalancer ip address>' to run the client demo.

if it works right, you will get "s.m.h.test.SyncEventHandlerDemo - TEST MESSAGE : Hi Server!" in your slave log after you run the client demo.
