#include <core.p4>
#include <v1model.p4>

typedef bit<9> port_t;
typedef bit<48> mac_t;
@controller_header("packet_in") header packet_in_header_t {
    bit<7> _padding;
    bit<9> ingress_port;
}

@controller_header("packet_out") header packet_out_header_t {
    bit<7> _padding;
    bit<9> egress_port;
}

header ethernet_h {
    mac_t   dst_addr;
    mac_t   src_addr;
    bit<16> ether_type;
}

struct headers_t {
    packet_in_header_t  packet_in;
    packet_out_header_t packet_out;
    ethernet_h          ethernet;
}

struct local_metadata_t {
}

control table0_control(inout headers_t hdr, inout local_metadata_t local_metadata, inout standard_metadata_t standard_metadata) {
    action send(port_t port) {
        standard_metadata.egress_spec = port;
    }
    action send_to_cpu() {
        standard_metadata.egress_spec = 255;
    }
    action drop() {
        mark_to_drop(standard_metadata);
    }
    table table0 {
        key = {
            standard_metadata.ingress_port: ternary;
            hdr.ethernet.src_addr         : ternary;
            hdr.ethernet.dst_addr         : ternary;
            hdr.ethernet.ether_type       : ternary;
        }
        actions = {
            send;
            send_to_cpu;
            drop;
        }
        default_action = drop;
        size = 512;
    }
    apply {
        table0.apply();
    }
}

parser MyParser(packet_in packet, out headers_t hdr, inout local_metadata_t meta, inout standard_metadata_t standard_metadata) {
    state start {
        transition select(standard_metadata.ingress_port) {
            255: parse_packet_out;
            default: parse_ethernet;
        }
    }
    state parse_packet_out {
        packet.extract(hdr.packet_out);
        transition parse_ethernet;
    }
    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition accept;
    }
}

control MyIngress(inout headers_t hdr, inout local_metadata_t meta, inout standard_metadata_t standard_metadata) {
    apply {
        if (standard_metadata.ingress_port == 255) {
            standard_metadata.egress_spec = hdr.packet_out.egress_port;
            hdr.packet_out.setInvalid();
            exit;
        }
        else {
            table0_control.apply(hdr, meta, standard_metadata);
        }
    }
}

control MyVerifyChecksum(inout headers_t hdr, inout local_metadata_t meta) {
    apply {
    }
}

control MyEgress(inout headers_t hdr, inout local_metadata_t meta, inout standard_metadata_t standard_metadata) {
    apply {
        if (standard_metadata.egress_port == 255) {
            hdr.packet_in.setValid();
            hdr.packet_in.ingress_port = standard_metadata.ingress_port;
            hdr.packet_in._padding = 0;
        }
    }
}

control MyComputeChecksum(inout headers_t hdr, inout local_metadata_t meta) {
    apply {
    }
}

control MyDeparser(packet_out packet, in headers_t hdr) {
    apply {
        packet.emit(hdr.packet_in);
        packet.emit(hdr.ethernet);
    }
}

V1Switch(MyParser(), MyVerifyChecksum(), MyIngress(), MyEgress(), MyComputeChecksum(), MyDeparser()) main;

