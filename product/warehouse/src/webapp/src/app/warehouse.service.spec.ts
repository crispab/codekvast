import {WarehouseService} from './warehouse.service';
import {ConfigService} from './config.service';

let warehouse: WarehouseService;

describe('WarehouseService', () => {

    beforeEach(() => {
        warehouse = new WarehouseService(null, new ConfigService());
    });

    it('should construct a get methods search without parameters', () => {
        expect(warehouse.constructGetMethodsSearch(undefined, undefined)).toBe('');
    });

    it('should construct a get methods search with only signature parameter', () => {
        expect(warehouse.constructGetMethodsSearch('sig', undefined)).toBe('signature=sig');
    });

    it('should construct a get methods search with only blank signature parameter', () => {
        expect(warehouse.constructGetMethodsSearch(' ', undefined)).toBe('');
    });

    it('should construct a get methods search with only maxResults parameter', () => {
        expect(warehouse.constructGetMethodsSearch(undefined, 100)).toBe('maxResults=100');
    });

    it('should construct a get methods search with blank signature and maxResults parameter', () => {
        expect(warehouse.constructGetMethodsSearch(' ', 100)).toBe('maxResults=100');
    });

    it('should construct a get methods search with both signature maxResults parameter', () => {
        expect(warehouse.constructGetMethodsSearch('sig', 100)).toBe('signature=sig&maxResults=100');
    });

});
