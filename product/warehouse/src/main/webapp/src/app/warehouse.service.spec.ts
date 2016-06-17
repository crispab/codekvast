import {provide} from '@angular/core';
import {Http, Response, BaseRequestOptions, Headers} from '@angular/http';
import {describe, expect, it, inject, beforeEach, beforeEachProviders} from '@angular/core/testing';
import {MockBackend} from '@angular/http/testing';
import {ConfigService} from './config.service';
import {WarehouseService} from './warehouse.service';

describe('WarehouseService', () => {

    let mockBackend, warehouse;

    beforeEachProviders(() => [
        ConfigService,
        WarehouseService,
        MockBackend,
        BaseRequestOptions,
        provide(Http, {
            deps: [MockBackend, BaseRequestOptions],
            useFactory: (backend, options) => new Http(backend, options)
        })
    ]);

    beforeEach(inject([MockBackend, WarehouseService], (_mockBackend, _warehouse) => {
        mockBackend = _mockBackend;
        warehouse = _warehouse;
    }));

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

    it('should return mocked response', done => {
        let response = {methods: ['foo', 'bar']};

        mockBackend.connections.subscribe(connection => {
            connection.mockRespond(new Response({
                url: '',
                merge: null,
                status: 200,
                headers: new Headers(),
                body: JSON.stringify(response)
            }));
        });

        warehouse.getMethods().subscribe(data => {
            expect(data.methods.length).toBe(2);
            expect(data.methods).toContain('foo');
            expect(data.methods).toContain('bar');
            done();
        });
    });

});
