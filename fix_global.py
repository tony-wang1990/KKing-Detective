import json

file_path = 'src/main/java/com/tony/kingdetective/telegram/handler/impl/GlobalInstanceSummaryHandler.java'
with open(file_path, 'r', encoding='utf-8') as f:
    text = f.read()

text = text.replace('import com.tony.kingdetective.bean.dto.InstanceCfgDTO;', 'import com.oracle.bmc.core.model.Vnic;')
text = text.replace('List<InstanceCfgDTO> instances = new ArrayList<>();', 'List<Tuple2<Instance, String>> instances = new ArrayList<>();')

old_loop = '''            for (InstanceCfgDTO dto : summary.instances) {
                String state = dto.getInstance().getLifecycleState().getValue();
                String ip = dto.getPublicIp() != null && !dto.getPublicIp().isEmpty() ? dto.getPublicIp() : "无公网";
                String shape = dto.getInstance().getShape();
                if (shape.contains("Micro")) shape = "ARM";
                else if (shape.contains("E4") || shape.contains("E3")) shape = "AMD";

                boolean isRunning = "RUNNING".equals(state);
                if (isRunning) totalRunning++;
                else totalStopped++;

                sb.append("   ").append(isRunning ? "" : "")
                  .append(" ").append(truncateString(dto.getInstance().getDisplayName(), 12)).append("")
                  .append(" | ").append(ip).append("")
                  .append(" | ").append(shape).append("\\n");
            }'''

new_loop = '''            for (Tuple2<Instance, String> tuple : summary.instances) {
                Instance instance = tuple.getT1();
                String ip = tuple.getT2();
                String state = instance.getLifecycleState() != null ? instance.getLifecycleState().getValue() : "UNKNOWN";
                String shape = instance.getShape();
                if (shape != null) {
                    if (shape.contains("Micro")) shape = "ARM";
                    else if (shape.contains("E4") || shape.contains("E3") || shape.contains("A1")) shape = "AMD/ARM";
                }

                boolean isRunning = "RUNNING".equals(state);
                if (isRunning) totalRunning++;
                else totalStopped++;

                sb.append("   ").append(isRunning ? "" : "")
                  .append(" ").append(truncateString(instance.getDisplayName(), 12)).append("")
                  .append(" | ").append(ip).append("")
                  .append(" | ").append(shape).append("\\n");
            }'''
text = text.replace(old_loop, new_loop)

old_query = '''                    InstanceCfgDTO cfg = instanceService.getInstanceCfgInfo(dto, instance.getId());
                    if (cfg != null) {
                        summary.instances.add(cfg);
                    }'''
new_query = '''                    
                    // 获取Public IP
                    String publicIp = "无公网";
                    try {
                        var vnicAttachments = fetcher.getComputeClient().listVnicAttachments(
                            com.oracle.bmc.core.requests.ListVnicAttachmentsRequest.builder()
                                .compartmentId(fetcher.getCompartmentId())
                                .instanceId(instance.getId())
                                .build()
                        ).getItems();
                        if (!vnicAttachments.isEmpty()) {
                            Vnic vnic = fetcher.getVirtualNetworkClient().getVnic(
                                com.oracle.bmc.core.requests.GetVnicRequest.builder()
                                    .vnicId(vnicAttachments.get(0).getVnicId())
                                    .build()
                            ).getVnic();
                            if (vnic.getPublicIp() != null) {
                                publicIp = vnic.getPublicIp();
                            }
                        }
                    } catch (Exception ignore) {}
                    
                    summary.instances.add(new Tuple2<>(instance, publicIp));'''
text = text.replace(old_query, new_query)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(text)
print("Finished modifying GlobalInstanceSummaryHandler.java")
