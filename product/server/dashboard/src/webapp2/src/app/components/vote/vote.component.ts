import {Component, Input, OnInit} from '@angular/core';
import {StateService} from '../../../../../webapp/src/app/services/state.service';
import {Router} from '@angular/router';

class VoteState {
    vote: boolean = undefined;

    constructor(private router: Router, private feature: string) {
        console.log('[ck dashboard] Created a vote state for ' + feature);
    }

    doVote(choice: boolean) {
        console.log(`[ck dashboard] Voted ${choice} for ${this.feature}`);
        this.vote = choice;
        //noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['/vote-result', this.feature, this.vote]);
    }

    hasVoted() {
        return this.vote !== undefined;
    }

    hasVotedFor(choice: boolean) {
        return this.vote === choice;
    }

    resetVote() {
        console.log(`[ck dashboard] Withdraw vote=${this.vote} for ${this.feature}`);
        //noinspection JSIgnoredPromiseFromCall
        this.router.navigate(['/vote-result', this.feature, 'withdraw-' + this.vote]);
        this.vote = undefined;
    }
}

@Component({
  selector: 'app-vote-for',
  templateUrl: './vote.component.html',
})
export class VoteComponent implements OnInit {
    state: VoteState;

    @Input('feature') featureName: string;

    constructor(private router: Router, private stateService: StateService) {
    }

    ngOnInit(): void {
        this.state = this.stateService.getState('vote_' + this.featureName, () => new VoteState(this.router, this.featureName));
    }
}
