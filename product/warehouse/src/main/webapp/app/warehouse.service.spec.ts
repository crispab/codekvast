import {provide} from 'angular2/core';
import {Http, Response, BaseRequestOptions, Headers} from 'angular2/http';
import {describe, expect, it, inject, beforeEach, beforeEachProviders} from 'angular2/testing';
import {MockBackend} from 'angular2/http/testing';
import {ConfigService} from './config.service';
import {WarehouseService} from './warehouse.service';

describe('WarehouseService', () => {

    let mockBackend, warehouse;

    //setup
    beforeEachProviders(() => [ConfigService, WarehouseService, MockBackend, BaseRequestOptions,
        provide(Http, {deps: [MockBackend, BaseRequestOptions], useFactory: (backend, options) => new Http(backend, options)})
    ]);

    beforeEach(inject([MockBackend, WarehouseService], (_mockBackend, _warehouse) => {
        mockBackend = _mockBackend;
        warehouse = _warehouse;
    }));

    it('should construct a get methods url without parameters', done => {
        expect(warehouse.constructGetMethodsUrl()).toBe('api/v1/methods');
        done();
    });

    it('should construct a get methods url with only signature parameter', done => {
        expect(warehouse.constructGetMethodsUrl("sig")).toBe('api/v1/methods\?signature=sig');
        done();
    });

    it('should construct a get methods url with only empty signature parameter', done => {
        expect(warehouse.constructGetMethodsUrl('')).toBe('api/v1/methods');
        done();
    });

    it('should construct a get methods url with only maxResults parameter', done => {
        expect(warehouse.constructGetMethodsUrl(undefined, 100)).toBe('api/v1/methods\?maxResults=100');
        done();
    });

    it('should construct a get methods url with both signature maxResults parameter', done => {
        expect(warehouse.constructGetMethodsUrl("sig", 100)).toBe('api/v1/methods\?signature=sig&maxResults=100');
        done();
    });

    it('should return mocked response', done => {
        let response = {data: ["ru", "es"]};

        mockBackend.connections.subscribe(connection => {
            connection.mockRespond(new Response({
                url: '',
                merge: null,
                status: 200,
                headers: new Headers(),
                body: JSON.stringify(response)}));
        });

        warehouse.getMethods().subscribe(languages => {
            expect(languages.length).toBe(2);
            expect(languages).toContain('ru');
            expect(languages).toContain('es');
            done();
        });
    });

});
