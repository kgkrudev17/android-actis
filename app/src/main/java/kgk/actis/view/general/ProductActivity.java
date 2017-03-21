package kgk.actis.view.general;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kgk.actis.R;
import kgk.actis.model.product.Product;
import kgk.actis.model.product.ProductFactory;
import kgk.actis.model.product.ProductType;
import kgk.actis.util.AppController;
import kgk.actis.view.general.adapter.ProductListAdapter;

public class ProductActivity extends AppCompatActivity {

    @Bind(R.id.productList) RecyclerView productListRecyclerView;

    private static boolean monitoringModuleInitialized;

    private ProgressDialog progressDialog;

    ////

    @OnClick(R.id.back)
    public void onBackImageButtonClick() {
        finish();
    }

    ////

    public static final String KEY_PRODUCT_TYPE = "product_type";

    private static final String TAG = ProductActivity.class.getSimpleName();

    ////

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product);
        ButterKnife.bind(this);
        prepareProductList();
    }

    ////

    private void prepareProductList() {
        productListRecyclerView.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        productListRecyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        ProductListAdapter adapter = new ProductListAdapter(this,
                ProductFactory.provideProductList(),
                new ProductListAdapter.OnItemClickListener() {
                    @Override
                    public void onClick(Product product) {
                        Intent startActivityIntent = null;

                        if (!product.getStatus()) {
//                            Toast.makeText(
//                                    ProductActivity.this,
//                                    "У вас нет соответствующих устройтсв",
//                                    Toast.LENGTH_LONG).show();
                            return;
                        }

                        switch (product.getProductType()) {
                            case Actis:
                                AppController.getInstance().setActiveProductType(ProductType.Actis);
                                startActivityIntent = new Intent(ProductActivity.this, DeviceListActivity.class);
                                break;
                            case Monitoring:
                                AppController.getInstance().setActiveProductType(ProductType.Monitoring);

                                /**
                                 * Monitoring manager initialization sequence:
                                 *
                                 * 1) Get monitroing entites from device store
                                 * 2) Get user data from server
                                 * 3) Update location data from server
                                 * 4) Start map activity
                                 */

                                toggleProgressDialog(true);
                                return;
                            case Generator:
                                AppController.getInstance().setActiveProductType(ProductType.Generator);
                                startActivityIntent = new Intent(ProductActivity.this, DeviceListActivity.class);
                                break;
                        }

                        startActivity(startActivityIntent);
                    }
                });
        productListRecyclerView.setAdapter(adapter);
        productListRecyclerView.addOnItemTouchListener(adapter);
    }

    private void toggleProgressDialog(boolean status) {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.monitoring_downloading_data));
            progressDialog.setIndeterminate(true);
            progressDialog.setCanceledOnTouchOutside(false);
        }

        if (status) progressDialog.show();
        else progressDialog.dismiss();
    }
}

















































