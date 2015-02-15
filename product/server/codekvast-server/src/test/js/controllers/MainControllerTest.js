'use strict';

describe('MainController', function () {

    // load the controller's module
    beforeEach(module('codekvastApp'));

    var MainController,
        StompService,
        scope,
        now = Date.now();

    // Initialize the controller and a mock scope
    beforeEach(inject(function ($controller, $rootScope, _StompService_) {
        scope = $rootScope.$new();
        StompService = _StompService_;
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

    it('should handle stompStatus events', function () {
        expect(scope.jumbotronMessage).toBe("Disconnected from server");

        StompService.onConnected();
        expect(scope.jumbotronMessage).toBe("Waiting for data...");

        StompService.onCollectorStatusMessage({body: '{}'});
        expect(scope.jumbotronMessage).toBeUndefined();

        StompService.onDisconnect();
        expect(scope.jumbotronMessage).toBe('Disconnected');
    });

    it('should handle collectorStatus event', function () {
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
        StompService.onCollectorStatusMessage(data);
        expect(scope.jumbotronMessage).toBeUndefined();
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
