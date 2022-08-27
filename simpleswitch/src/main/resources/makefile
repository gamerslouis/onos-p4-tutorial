ROOT_DIR:=$(shell dirname $(realpath $(firstword $(MAKEFILE_LIST))))/..

all: p4 constants

p4: simpleswitch.p4
	@./bmv2-compile.sh "simpleswitch" ""

constants:
	docker run -v $(ONOS_ROOT):/onos -w /onos/tools/dev/bin \
		-v $(ROOT_DIR):/source \
		--entrypoint ./onos-gen-p4-constants opennetworking/p4mn:stable \
		-o /source/java/me/louisif/pipelines/simpleswitch/SimpleswitchConstants.java \
		simpleswitch /source/resources/p4c-out/bmv2/simpleswitch_p4info.txt

clean:
	rm -rf p4c-out/bmv2/*
