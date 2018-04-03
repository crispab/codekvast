import {AgePipe} from '../../pipes/age.pipe';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {StatusData} from '../../model/status/StatusData';
import {Subscription} from 'rxjs/Subscription';
import {TimerObservable} from 'rxjs/observable/TimerObservable';

export class CollectionStatusComponentState {
    static KEY = 'collection-status';

    data: StatusData;
    errorMessage: string;
    autoRefresh = true;
    showTerminatedAgents = false;
    refreshIntervalSeconds = 60;
    private timerSubscription: Subscription;

    constructor(private agePipe: AgePipe, private api: DashboardApiService) {
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

    autoRefreshButtonClasses() {
        return {
            'fas': true,
            'fa-pause': this.autoRefresh,
            'fa-play': !this.autoRefresh
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
        return this.autoRefresh ? 'Pause auto-refresh' : 'Resume auto-refresh';
    }

    private startAutoRefresh() {
        let timer = TimerObservable.create(0, this.refreshIntervalSeconds * 1000);
        this.timerSubscription = timer.subscribe((tick: number) => {
            console.log('[ck dashboard] Doing auto-refresh #%o', tick);
            this.refreshNow();
        });
    }

    private stopAutoRefresh() {
        this.timerSubscription.unsubscribe();
    }

    updateRefreshTimer() {
        this.refreshIntervalSeconds = Math.max(10, this.refreshIntervalSeconds);
        console.log('[ck dashboard] New refreshIntervalSeconds: %o', this.refreshIntervalSeconds);
        if (this.autoRefresh) {
            this.stopAutoRefresh();
            this.startAutoRefresh();
        }
    }

    refreshNow() {
        this.api
            .getStatus()
            .subscribe(data => {
                this.data = data;
                this.errorMessage = undefined;

            }, error => {
                this.data = undefined;
                this.errorMessage = error.statusText ? error.statusText : error;
            });
    }

    communicationFailure() {
        let now = this.agePipe.transform(new Date(), 'shortTime');
        return now + ': Communication failure'
    }

    getFilteredAgents() {
        if (this.data.agents) {
            return this.data.agents.filter(a => this.showTerminatedAgents || a.agentAlive);
        }
        return null;
    }

}
