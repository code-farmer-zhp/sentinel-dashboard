package com.alibaba.csp.sentinel.dashboard.discovery.nacos;

import com.alibaba.csp.sentinel.dashboard.discovery.AppInfo;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineDiscovery;
import com.alibaba.csp.sentinel.dashboard.discovery.MachineInfo;
import com.alibaba.nacos.api.exception.NacosException;
import com.alibaba.nacos.api.naming.NamingService;
import com.alibaba.nacos.api.naming.pojo.Instance;
import com.alibaba.nacos.api.naming.pojo.ListView;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NacosAppManagement implements MachineDiscovery {

    @Autowired
    private NamingService namingService;

    @Override
    public List<String> getAppNames() {
        return getServices();
    }

    private List<String> getServices() {
        try {
            ListView<String> servicesOfServer = namingService.getServicesOfServer(0, 500);
            return servicesOfServer.getData().stream().filter(v -> !v.contains(":")).collect(Collectors.toList());
        } catch (NacosException e) {
            log.error("查询服务列表异常", e);
            return new ArrayList<>();
        }
    }

    @Override
    public Set<AppInfo> getBriefApps() {
        List<String> services = getServices();
        Set<AppInfo> appInfos = new HashSet<>();
        List<CompletableFuture<Boolean>> completableFutures = new ArrayList<>();
        for (String service : services) {
            CompletableFuture<Boolean> supplyAsync = CompletableFuture.supplyAsync(() -> {
                AppInfo detailApp = getDetailApp(service);
                appInfos.add(detailApp);
                return true;
            });
            completableFutures.add(supplyAsync);
        }
        try {
            CompletableFuture.allOf(completableFutures.toArray(new CompletableFuture[0])).get();
        } catch (Exception e) {
            log.error("查询服务实例异常", e);
        }
        return appInfos;
    }

    @Override
    public AppInfo getDetailApp(String app) {
        try {
            List<Instance> allInstances = namingService.getAllInstances(app);

            AppInfo appInfo = new AppInfo();
            appInfo.setApp(app);
            appInfo.setAppType(0);

            for (Instance instance : allInstances) {
                MachineInfo machineInfo = new MachineInfo();
                machineInfo.setPort(instance.getPort());
                machineInfo.setApp(app);
                machineInfo.setAppType(0);
                machineInfo.setHostname(instance.getServiceName());
                machineInfo.setIp(instance.getIp());
                machineInfo.setHealthy(instance.isHealthy());
                machineInfo.setDead(instance.isEnabled());
                appInfo.addMachine(machineInfo);
            }
            return appInfo;
        } catch (NacosException e) {
            log.info("查询实例异常", e);
            return null;
        }
    }

    @Override
    public void removeApp(String app) {

    }

    @Override
    public long addMachine(MachineInfo machineInfo) {
        return 0;
    }

    @Override
    public boolean removeMachine(String app, String ip, int port) {
        return false;
    }
}
