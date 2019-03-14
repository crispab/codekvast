import {AgePipe} from '../../pipes/age.pipe';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {StatusData} from '../../model/status/StatusData';
import {Subscription, timer} from 'rxjs';

export class CollectionStatusComponentState {
    static KEY = 'collection-status';

    data: StatusData;
    errorMessage: string;
    autoRefresh = true;
    showTerminatedAgents = false;
    refreshIntervalSeconds = 60;

    applicationFilter = '';
    environmentFilter = '';
    hostnameFilter = '';

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
        };
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
        return now + ': Communication failure';
    }

    getEnvironments() {
        return this.data.environments;
    }

    getVisibleApplications() {
        if (this.data.applications) {
            // @formatter:off
            return this.data.applications.filter(a =>
                `${a.appName}`.toLowerCase().indexOf(this.applicationFilter.toLowerCase()) >= 0
                && a.environment.toLowerCase().indexOf(this.environmentFilter.toLowerCase()) >= 0);
            // @formatter:on
        }
        return null;
    }

    getFilteredAgents() {
        if (this.data.agents) {
            // @formatter:off
            return this.data.agents.filter(a =>
                `${a.appName} ${a.appVersion}`.toLowerCase().indexOf(this.applicationFilter.toLowerCase()) >= 0
                && a.environment.toLowerCase().indexOf(this.environmentFilter.toLowerCase()) >= 0
                && a.hostname.toLowerCase().indexOf(this.hostnameFilter.toLowerCase()) >= 0);
            // @formatter:on
        }
        return null;
    }

    getVisibleAgents() {
        return this.getFilteredAgents().filter(a => (this.showTerminatedAgents || a.agentAlive));
    }

    numTerminatedAgents() {
        if (this.data.agents) {
            return this.data.agents.filter(a => !a.agentAlive).length;
        }
        return null;
    }

    numTerminatedFilteredAgents() {
        if (this.getFilteredAgents()) {
            return this.getFilteredAgents().filter(a => !a.agentAlive).length;
        }
        return null;
    }

    private startAutoRefresh() {
        this.timerSubscription = timer(0, this.refreshIntervalSeconds * 1000).subscribe((tick: number) => {
            console.log('[ck dashboard] Doing auto-refresh #%o', tick);
            this.refreshNow();
        });
    }

    private stopAutoRefresh() {
        this.timerSubscription.unsubscribe();
    }

}
