import {Component, Input, OnInit} from '@angular/core';
import {Router} from '@angular/router';
import {StateService} from '../../services/state.service';

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

    @Input() feature: string;

    constructor(private router: Router, private stateService: StateService) {
    }

    ngOnInit(): void {
        this.state = this.stateService.getState('vote_' + this.feature, () => new VoteState(this.router, this.feature));
    }
}
