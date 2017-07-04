import {StatusData} from '../../model/status/StatusData';
import {WarehouseService} from '../../services/warehouse.service';
import {AgePipe} from '../../pipes/age.pipe';
import {Subscription} from 'rxjs/Subscription';
import {TimerObservable} from 'rxjs/observable/TimerObservable';

export class CollectionStatusComponentState {
    static KEY = 'collection-status';

    data: StatusData;
    errorMessage: string;
    autoRefresh = true;
    refreshIntervalSeconds = 60;
    private timerSubscription: Subscription;

    constructor(private agePipe: AgePipe, private warehouse: WarehouseService) {
    }

    init() {
        if (this.autoRefresh) {
            this.startAutoRefresh();
        } else {
            this.refreshNow();
        }
    }

    destroy() {
        this.stopAutoRefresh();
    }

    toggleAutoRefreshButtonClasses() {
        return {
            'mr-2': true,
            'fa': true,
            'fa-pause': this.autoRefresh,
            'fa-play': !this.autoRefresh,
        }
    }

    toggleAutoRefresh() {
        this.autoRefresh = !this.autoRefresh;
        if (!this.autoRefresh) {
            this.stopAutoRefresh();
        } else {
            this.startAutoRefresh();
        }
    }

    autoRefreshButtonText() {
        return this.autoRefresh ? 'Pause' : 'Resume';
    }

    private startAutoRefresh() {
        let timer = TimerObservable.create(0, this.refreshIntervalSeconds * 1000);
        this.timerSubscription = timer.subscribe((tick: number) => {
            console.log('Doing auto-refresh #%o', tick);
            this.refreshNow();
        });
    }

    private stopAutoRefresh() {
        this.timerSubscription.unsubscribe();
    }

    updateRefreshTimer() {
        this.refreshIntervalSeconds = Math.max(10, this.refreshIntervalSeconds);
        console.log('New refreshIntervalSeconds: %o', this.refreshIntervalSeconds);
        if (this.autoRefresh) {
            this.stopAutoRefresh();
            this.startAutoRefresh();
        }
    }

    refreshNow() {
        this.warehouse
            .getStatus()
            .subscribe(data => {
                this.data = data;
                this.errorMessage = undefined;

                // TODO remove after debugging
                // this.data.collectedDays = 29;
                // this.data.maxCollectionPeriodDays = 30;
                // end
            }, error => {
                this.data = undefined;
                this.errorMessage = error.statusText ? error.statusText : error;
            }, () => console.log('getStatus() complete'));
    }

    communicationFailure() {
        let now = this.agePipe.transform(new Date(), 'shortTime');
        return now + ': Communication failure'
    }

}
