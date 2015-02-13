'use strict';

describe('Controller: MainController', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var MainController,
        scope,
        now = Date.now();

    // Initialize the controller and a mock scope
    beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        MainController = $controller('MainController', {
            $scope: scope
        });
    }));

    it('should have an empty list of signatures', function () {
        expect(scope.signatures.length).toBe(0);
    });

    it('should have an undefined collectorStatus', function () {
        expect(scope.collectorStatus).toBeUndefined();
    });

    it('should have an undefined progress', function () {
        expect(scope.progress).toBeUndefined();
    });

    it('should calculate age', function () {
        expect(getAge(now, 0)).toBe("");
        expect(getAge(now, now)).toBe("");
        expect(getAge(now, now - 29000)).toBe("29s");
        expect(getAge(now, now - 30000)).toBe("30s");
        expect(getAge(now, now - 31000)).toBe("31s");
        expect(getAge(now, now - 59000)).toBe("59s");
        expect(getAge(now, now - 60000)).toBe("1m");
        expect(getAge(now, now - 61000)).toBe("1m 1s");
        expect(getAge(now, now - 119000)).toBe("1m 59s");
        expect(getAge(now, now - 120000)).toBe("2m");
        expect(getAge(now, now - 121000)).toBe("2m 1s");
        expect(getAge(now, now - 59 * 60000)).toBe("59m");
        expect(getAge(now, now - 60 * 60000)).toBe("1h");
        expect(getAge(now, now - 24 * 60 * 60000)).toBe("1d");
        expect(getAge(now, now - 7 * 24 * 60 * 60000)).toBe("7d");
    })

    it('should handle collectorStatusMessage', function () {
        // given
        var data = {
            body: JSON.stringify({
                collectionStartedAtMillis: now - 3 * 24 * 3600 * 1000,
                updateReceivedAtMillis: now - 61000,
                collectors: [
                    {
                        collectorStartedAtMillis: now - 3 * 24 * 3600 * 1000,
                        updateReceivedAtMillis: now - 120000
                    },
                    {
                        collectorStartedAtMillis: now - 5 * 24 * 3600 * 1000,
                        updateReceivedAtMillis: now - 180000
                    }
                ]
            })
        };

        // when
        onCollectorStatusMessage(data);
        updateAges();

        // then
        expect(scope.collectorStatus.collectionAge).toBe("3d");
        expect(scope.collectorStatus.updateAge).toBe("1m 1s");
        expect(scope.collectorStatus.collectors[0].collectorAge).toBe("3d");
        expect(scope.collectorStatus.collectors[0].updateAge).toBe("2m");
        expect(scope.collectorStatus.collectors[1].collectorAge).toBe("5d");
        expect(scope.collectorStatus.collectors[1].updateAge).toBe("3m");
    });
});
