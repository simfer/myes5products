using my.bookshop as my from '../db/data-model';
using {GWSAMPLE_BASIC as external} from './external/GWSAMPLE_BASIC.csn';

service CatalogService {
    entity Books           as projection on my.Books;

    @cds.persistence.skip
    entity MyProduct as projection on external.ProductSet {
        ProductID,Name,Description,Price
    };
}
