import {AgePipe} from '../../pipes/age.pipe';
import {DashboardApiService} from '../../services/dashboard-api.service';
import {StatusData} from '../../model/status/StatusData';
import {Subscription} from 'rxjs/Subscription';
import {TimerObservable} from 'rxjs/observable/TimerObservable';
import {Agent} from '../../model/status/Agent';
import {isNullOrUndefined} from 'util';

export class CollectionStatusComponentState {
    static KEY = 'collection-status';

    data: StatusData;
    errorMessage: string;
    autoRefresh = true;
    showTerminatedAgents = false;
    refreshIntervalSeconds = 60;
    selectAllTerminatedAgents = false;
    selectTerminatedAgentsOlderThanDays = 30;

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
                this.selectAllTerminatedAgents = false;
            }, error => {
                this.data = undefined;
                this.errorMessage = error.statusText ? error.statusText : error;
                this.selectAllTerminatedAgents = false;
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

    numTerminatedAgents() {
        if (this.data.agents) {
            return this.data.agents.filter(a => !a.agentAlive).length
        }
        return null;
    }

    numSelectedTerminatedAgents() {
        if (this.data.agents) {
            return this.data.agents.filter(a => a.selected).length
        }
        return null;
    }

    selectOrUnselectAllAgents() {
        if (this.data.agents) {
            if (this.autoRefresh) {
                this.toggleAutoRefresh();
            }
            this.data.agents.forEach(
                a => a.selected = this.selectAllTerminatedAgents && !a.agentAlive && isNullOrUndefined(a.deletionState))
        }
    }

    getTerminatedBefore(): Date {
        let d = new Date();
        d.setDate(d.getDate() - this.selectTerminatedAgentsOlderThanDays);
        return d;
    }

    selectOldTerminatedAgents() {
        if (this.data.agents) {
            if (this.autoRefresh) {
                this.toggleAutoRefresh();
            }
            this.data.agents.filter(a => !a.agentAlive && a.publishedAtMillis < this.getTerminatedBefore().getTime())
                .forEach(a => a.selected = true);
        }
    }

    deleteSelectedAgents() {
        if (this.data.agents) {
            this.data.agents.filter(a => a.selected && !a.agentAlive).forEach(a => this.deleteAgent(a))
        }
    }

    private deleteAgent(agent: Agent) {
        agent.deletionState = 1;
        this.api.deleteAgent(agent.agentId, agent.jvmId).subscribe(() => {
            agent.deletionState = 2;
            agent.selected = false;
        });
    }
}
