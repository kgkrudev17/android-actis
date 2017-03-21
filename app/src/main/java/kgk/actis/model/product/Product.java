package kgk.actis.model.product;

import android.graphics.drawable.Drawable;

public class Product {

    private ProductType productType;
    private Drawable image;
    private Drawable inactiveImage;
    private String title;
    private String description;
    private boolean status;

    ////

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public Drawable getImage() {
        return image;
    }

    public void setImage(Drawable image) {
        this.image = image;
    }

    public Drawable getInactiveImage() {
        return inactiveImage;
    }

    public void setInactiveImage(Drawable inactiveImage) {
        this.inactiveImage = inactiveImage;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean getStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }
}
