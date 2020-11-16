package my.company.myes5products.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.sap.cds.services.cds.CdsReadEventContext;
import com.sap.cds.services.cds.CdsService;
import com.sap.cds.services.handler.EventHandler;
import com.sap.cds.services.handler.annotations.After;
import com.sap.cds.services.handler.annotations.On;
import com.sap.cds.services.handler.annotations.ServiceName;
import com.sap.cloud.sdk.cloudplatform.connectivity.DestinationAccessor;
import com.sap.cloud.sdk.cloudplatform.connectivity.HttpDestination;
import com.sap.cloud.sdk.datamodel.odata.client.exception.ODataException;
import com.sap.cloud.sdk.s4hana.connectivity.DefaultErpHttpDestination;
import com.vdm.namespaces.gwsamplebasic.Product;
import com.vdm.services.DefaultGWSAMPLEBASICService;

import cds.gen.catalogservice.CatalogService_;
import cds.gen.catalogservice.MyProduct;
import cds.gen.catalogservice.Books;

@Component
@ServiceName(CatalogService_.CDS_NAME)
public class CatalogServiceHandler implements EventHandler {
    private static final String DESTINATION_HEADER_KEY = "es5";
    private final Logger log = LoggerFactory.getLogger(this.getClass());

	@After(event = CdsService.EVENT_READ, entity = "CatalogService.Books")
	public void discountBooks(Stream<Books> books) {
		books.filter(b -> b.getTitle() != null && b.getStock() != null)
		.filter(b -> b.getStock() > 200)
		.forEach(b -> b.setTitle(b.getTitle() + " (discounted)"));
	}

	@On(event = CdsService.EVENT_READ, entity = "CatalogService.MyProduct")
    public void getMyProducts(CdsReadEventContext context) throws ODataException {
		log.info("Entering " + getClass().getSimpleName() + ":getMyProducts");
        System.out.println("Entering " + getClass().getSimpleName() + ":getMyProducts");

        // Get name of destination for ECC

        final Map<Object, Map<String, Object>> result = new HashMap<>();

        try {
            HttpDestination dest = DestinationAccessor.getDestination(DESTINATION_HEADER_KEY).asHttp();
            dest.decorate(DefaultErpHttpDestination::new);

            final List<Product> products = new DefaultGWSAMPLEBASICService().getAllProduct()
                    .select(Product.ALL_FIELDS).top(5).executeRequest(dest);

            final List<MyProduct> capProducts = new ArrayList<>();

            for (final Product p : products) {
                final MyProduct capProduct = com.sap.cds.Struct
                        .create(MyProduct.class);

                System.out.println(p.getProductID());

                capProduct.setProductID(p.getProductID());
                capProduct.setDescription(p.getProdDescrip());
                capProduct.setName(p.getProductName());
                capProduct.setPrice(p.getPrice());

                capProducts.add(capProduct);
            }

            capProducts.forEach(capProduct -> {
                result.put(capProduct.getProductID(), capProduct);
            });
        } catch (ODataException e) {
            log.error(e.getMessage());
        }

        context.setResult(result.values());


	}

}

