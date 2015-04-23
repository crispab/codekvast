'use strict';

describe('DateService', function () {

    // load the app module
    beforeEach(module('codekvastApp'));

    var DateService;

    // Initialize the service to test
    beforeEach(inject(function (_DateService_) {
        DateService = _DateService_;
    }));

    it('should calculate pretty duration', function () {
        expect(DateService.prettyDuration(0)).toBe("");
        expect(DateService.prettyDuration(999)).toBe("");
        expect(DateService.prettyDuration(1000)).toBe("1s");
        expect(DateService.prettyDuration(1001)).toBe("1s");
        expect(DateService.prettyDuration(29000)).toBe("29s");
        expect(DateService.prettyDuration(30000)).toBe("30s");
        expect(DateService.prettyDuration(31000)).toBe("31s");
        expect(DateService.prettyDuration(32999)).toBe("32s");
        expect(DateService.prettyDuration(59000)).toBe("59s");
        expect(DateService.prettyDuration(60000)).toBe("1m");
        expect(DateService.prettyDuration(61000)).toBe("1m 1s");
        expect(DateService.prettyDuration(119000)).toBe("1m 59s");
        expect(DateService.prettyDuration(120000)).toBe("2m");
        expect(DateService.prettyDuration(121000)).toBe("2m 1s");
        expect(DateService.prettyDuration(59 * 60000)).toBe("59m");
        expect(DateService.prettyDuration(60 * 60000)).toBe("1h");
        expect(DateService.prettyDuration(24 * 60 * 60000)).toBe("1d");
        expect(DateService.prettyDuration(7 * 24 * 60 * 60000 - 2 * 60 * 60000 - 60000 - 30000)).toBe("6d 21h 58m 30s");
    });
});
