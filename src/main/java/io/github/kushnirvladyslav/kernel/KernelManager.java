//package com.jopencl.core.kernel;
//
//import java.util.HashMap;
//import java.util.Map;
//
//public class KernelManager {
//    private static KernelManager kernelManager;
//
//    Map<String, Kernel> kernelMap; // Мапа для зберігання ядер
//
//    public static KernelManager getInstance() {
//        if (kernelManager == null) {
//            kernelManager = new KernelManager();
//        }
//
//        return kernelManager;
//    }
//
//    private KernelManager () {
//        kernelMap = new HashMap<>();
//    }
//
//
//    public void addKernel (String name, Kernel kernel) {
//        kernelMap.put(name, kernel);
//    }
//
//
//    public Kernel getKernel(String name) {
//        return kernelMap.get(name);
//    }
//
//
//    public void removeKernel (String name) {
//        if (!kernelMap.containsKey(name)) {
//            throw new IllegalArgumentException("A kernel with name \"" + name + "\" does not exist.");
//        } else {
//            kernelMap.remove(name);
//        }
//    }
//
//
//    public void destroy () {
//        for(Kernel kernel : kernelMap.values()) {
//            kernel.destroy();
//        }
//        kernelMap.clear();
//    }
//}
