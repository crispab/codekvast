import {provide} from 'angular2/core';
import {Http, Response, BaseRequestOptions, Headers} from 'angular2/http';
import {describe, expect, it, inject, beforeEach, beforeEachProviders} from 'angular2/testing';
import {MockBackend} from 'angular2/http/testing';
import {ConfigService} from './config.service';
import {WarehouseService} from './warehouse.service';

describe('WarehouseService', () => {

    let mockBackend, service;

    //setup
    beforeEachProviders(() => [ConfigService, WarehouseService, MockBackend, BaseRequestOptions, provide(Http, {
        useFactory: (backend, options) => new Http(backend, options), deps: [MockBackend, BaseRequestOptions]
    })]);

    beforeEach(inject([MockBackend, WarehouseService], (_mockBackend, _service) => {
        mockBackend = _mockBackend;
        service = _service;
    }))

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

        service.getMethods(null).subscribe(languages => {
            expect(languages.length).toBe(2);
            expect(languages).toContain('ru');
            expect(languages).toContain('es');
            done();
        });
    });

});
