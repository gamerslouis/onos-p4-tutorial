/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.louisif.pipelines.simpleswitch;

import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiPacketMetadataId;

/**
 * Constants for simpleswitch pipeline.
 */
public final class SimpleswitchConstants {

    // hide default constructor
    private SimpleswitchConstants() {
    }

    // Header field IDs
    public static final PiMatchFieldId HDR_HDR_ETHERNET_ETHER_TYPE =
            PiMatchFieldId.of("hdr.ethernet.ether_type");
    public static final PiMatchFieldId HDR_HDR_ETHERNET_SRC_ADDR =
            PiMatchFieldId.of("hdr.ethernet.src_addr");
    public static final PiMatchFieldId HDR_HDR_ETHERNET_DST_ADDR =
            PiMatchFieldId.of("hdr.ethernet.dst_addr");
    public static final PiMatchFieldId HDR_STANDARD_METADATA_INGRESS_PORT =
            PiMatchFieldId.of("standard_metadata.ingress_port");
    // Table IDs
    public static final PiTableId MY_INGRESS_TABLE0_CONTROL_TABLE0 =
            PiTableId.of("MyIngress.table0_control.table0");
    // Action IDs
    public static final PiActionId MY_INGRESS_TABLE0_CONTROL_SEND_TO_CPU =
            PiActionId.of("MyIngress.table0_control.send_to_cpu");
    public static final PiActionId MY_INGRESS_TABLE0_CONTROL_SEND =
            PiActionId.of("MyIngress.table0_control.send");
    public static final PiActionId MY_INGRESS_TABLE0_CONTROL_DROP =
            PiActionId.of("MyIngress.table0_control.drop");
    // Action Param IDs
    public static final PiActionParamId PORT = PiActionParamId.of("port");
    // Packet Metadata IDs
    public static final PiPacketMetadataId INGRESS_PORT =
            PiPacketMetadataId.of("ingress_port");
    public static final PiPacketMetadataId EGRESS_PORT =
            PiPacketMetadataId.of("egress_port");
}