import {WarehouseService} from './warehouse.service';
import {ConfigService} from './config.service';

let warehouse: WarehouseService;

describe('WarehouseService', () => {

    beforeEach(() => {
        warehouse = new WarehouseService(null, new ConfigService());
    });


    it('should construct a get methods search without parameters', done => {
        expect(warehouse.constructGetMethodsSearch(undefined, undefined)).toBe('');
        done();
    });

    it('should construct a get methods search with only signature parameter', done => {
        expect(warehouse.constructGetMethodsSearch("sig", undefined)).toBe('signature=sig');
        done();
    });

    it('should construct a get methods search with only blank signature parameter', done => {
        expect(warehouse.constructGetMethodsSearch(' ', undefined)).toBe('');
        done();
    });

    it('should construct a get methods search with only maxResults parameter', done => {
        expect(warehouse.constructGetMethodsSearch(undefined, 100)).toBe('maxResults=100');
        done();
    });

    it('should construct a get methods search with blank signature and maxResults parameter', done => {
        expect(warehouse.constructGetMethodsSearch(' ', 100)).toBe('maxResults=100');
        done();
    });

    it('should construct a get methods search with both signature maxResults parameter', done => {
        expect(warehouse.constructGetMethodsSearch("sig", 100)).toBe('signature=sig&maxResults=100');
        done();
    });

});
