import {ConfigService} from './config.service';
import {WarehouseService} from './warehouse.service';

let warehouse;

describe('WarehouseService', () => {

    beforeEach(() => {
        warehouse = new WarehouseService(null, new ConfigService());
    });

    it('should construct a get methods url without parameters', done => {
        expect(warehouse.constructGetMethodsUrl()).toBe('/api/v1/methods');
        done();
    });

    it('should construct a get methods url with only signature parameter', done => {
        expect(warehouse.constructGetMethodsUrl("sig")).toBe('/api/v1/methods\?signature=sig');
        done();
    });

    it('should construct a get methods url with only empty signature parameter', done => {
        expect(warehouse.constructGetMethodsUrl('')).toBe('/api/v1/methods');
        done();
    });

    it('should construct a get methods url with only maxResults parameter', done => {
        expect(warehouse.constructGetMethodsUrl(undefined, 100)).toBe('/api/v1/methods\?maxResults=100');
        done();
    });

    it('should construct a get methods url with both signature maxResults parameter', done => {
        expect(warehouse.constructGetMethodsUrl("sig", 100)).toBe('/api/v1/methods\?signature=sig&maxResults=100');
        done();
    });

});
