import {describe, expect, it, inject, beforeEach, beforeEachProviders} from 'angular2/testing';
import {DatePipe} from 'angular2/src/common/pipes/date_pipe';
import {CkDatePipe} from './ck-date.pipe';

describe('WarehouseService', () => {

    let pipe;
    let datePipe;

    beforeEachProviders(() => [DatePipe]);

    beforeEach(inject([DatePipe], (_datePipe) => {
        datePipe = _datePipe;
        pipe = new CkDatePipe(datePipe);
    }));

    it("Should return null for null", done => {
        expect(pipe.transform(null)).toBe(null);
        done();
    });

    it("Should return null for zero", done => {
        expect(pipe.transform(0)).toBe(null);
        done();
    });

    it("Should return delegate to DatePipe when no format", done => {
        let value = new Date();
        expect(pipe.transform(value)).toBe(datePipe.transform(value));
        done();
    });

    it("Should return delegate to DatePipe when unrecognized format", done => {
        let value = new Date();
        let pattern = 'shortDate'; // NOTE: not the default pattern 'mediumDate'
        expect(pipe.transform(value, pattern)).toBe(datePipe.transform(value, pattern));
        done();
    });

});
