package kgk.actis.model.product;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;
import kgk.actis.R;
import kgk.actis.dispatcher.Dispatcher;
import kgk.actis.model.Device;
import kgk.actis.stores.DeviceStore;
import kgk.actis.util.AppController;

public class ProductFactory {

    public static List<Product> provideProductList() {
        List<Product> products = new ArrayList<>();

        List<Device> devices = DeviceStore.getInstance(Dispatcher.getInstance(EventBus.getDefault()))
                .getDevices();
        Set<String> deviceTypes = new HashSet<>();
        for (Device device : devices) {
            deviceTypes.add(device.getType());
        }

        Product actisProduct = new Product();
        actisProduct.setProductType(ProductType.Actis);
        actisProduct.setImage(AppController.getInstance()
                .getResources().getDrawable(R.drawable.actis_product_icon));
        actisProduct.setInactiveImage(AppController.getInstance()
                .getResources().getDrawable(R.drawable.actis_product_icon_inactive));
        actisProduct.setTitle("\nActis");
        actisProduct.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                " Ut gravida eros tortor, at dapibus nulla commodo tincidunt. Phasellus posuere " +
                "ornare ex, nec elementum enim cursus vitae. Pellentesque habitant morbi tristique " +
                "senectus et netus et malesuada fames ac turpis egestas. Aenean ut finibus sem. " +
                "Pellentesque eget fermentum nisl.");
        if (deviceTypes.contains(AppController.ACTIS_DEVICE_TYPE)) {
            actisProduct.setStatus(true);
        }
        products.add(actisProduct);

        Product monitoringProduct = new Product();
        monitoringProduct.setProductType(ProductType.Monitoring);
        monitoringProduct.setTitle("\nМониторинг");
        monitoringProduct.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                " Ut gravida eros tortor, at dapibus nulla commodo tincidunt. Phasellus posuere " +
                "ornare ex, nec elementum enim cursus vitae. Pellentesque habitant morbi tristique " +
                "senectus et netus et malesuada fames ac turpis egestas. Aenean ut finibus sem. " +
                "Pellentesque eget fermentum nisl.");
        if (deviceTypes.contains(AppController.T5_DEVICE_TYPE) ||
                deviceTypes.contains(AppController.T6_DEVICE_TYPE)) {
            monitoringProduct.setStatus(true);
        }
        products.add(monitoringProduct);

//        Product generatorProduct = new Product();
//        generatorProduct.setProductType(ProductType.Generator);
//        generatorProduct.setImage(AppController.getInstance()
//                .getResources().getDrawable(R.drawable.generator_product_icon));
//        generatorProduct.setInactiveImage(AppController.getInstance()
//                .getResources().getDrawable(R.drawable.generator_product_icon_inactive));
//        generatorProduct.setTitle("Автомат ввода резерва");
//        generatorProduct.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
//                " Ut gravida eros tortor, at dapibus nulla commodo tincidunt. Phasellus posuere " +
//                "ornare ex, nec elementum enim cursus vitae. Pellentesque habitant morbi tristique " +
//                "senectus et netus et malesuada fames ac turpis egestas. Aenean ut finibus sem. " +
//                "Pellentesque eget fermentum nisl.");
//        if (deviceTypes.contains(AppController.GENERATOR_DEVICE_TYPE)) {
//            generatorProduct.setStatus(true);
//        }
//        products.add(generatorProduct);

        Product salesProduct = new Product();
        salesProduct.setProductType(ProductType.Sales);
        salesProduct.setImage(AppController.getInstance()
                .getResources().getDrawable(R.drawable.sales_product_icon_inactive));
        salesProduct.setInactiveImage(AppController.getInstance()
                .getResources().getDrawable(R.drawable.sales_product_icon_inactive));
        salesProduct.setTitle("Торговые представители");
        salesProduct.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                " Ut gravida eros tortor, at dapibus nulla commodo tincidunt. Phasellus posuere " +
                "ornare ex, nec elementum enim cursus vitae. Pellentesque habitant morbi tristique " +
                "senectus et netus et malesuada fames ac turpis egestas. Aenean ut finibus sem. " +
                "Pellentesque eget fermentum nisl.");
        salesProduct.setStatus(false);
        products.add(salesProduct);

        Product trackerProduct = new Product();
        trackerProduct.setProductType(ProductType.Tracker);
        trackerProduct.setTitle("\nТрекер");
        trackerProduct.setDescription("Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                " Ut gravida eros tortor, at dapibus nulla commodo tincidunt. Phasellus posuere " +
                "ornare ex, nec elementum enim cursus vitae. Pellentesque habitant morbi tristique " +
                "senectus et netus et malesuada fames ac turpis egestas. Aenean ut finibus sem. " +
                "Pellentesque eget fermentum nisl.");
        trackerProduct.setStatus(false);
        products.add(trackerProduct);
        return moveActiveProductsToStart(products);
    }

    private static List<Product> moveActiveProductsToStart(List<Product> products) {
        List<Product> sortedProducts = new ArrayList<>();
        for (Product product : products) {
            if (product.getStatus()) sortedProducts.add(0, product);
            else sortedProducts.add(product);
        }

        return sortedProducts;
    }
}