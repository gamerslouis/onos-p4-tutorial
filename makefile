build:
	cd simpleswitch && mvn clean install -DskipTests

install:
	onos-app localhost install! simpleswitch/target/simpleswitch-1.0-SNAPSHOT.oar

topology:
	docker run -v /tmp/p4mn:/tmp --privileged --rm -it -p 50001:50001 opennetworking/p4mn:stable

netcfg:
	onos-netcfg localhost /tmp/p4mn/bmv2-s1-netcfg.json
