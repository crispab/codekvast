import {NgModule} from '@angular/core';
import {RouterModule, Routes} from '@angular/router';
import {HomeComponent} from './home.component';
import {StatusComponent} from './status.component';
import {MethodsComponent} from './methods.component';
import {MethodDetailComponent} from './method-detail.component';

const routes: Routes = [
    {
        path: '',
        redirectTo: 'home',
        pathMatch: 'full'
    }, {
        path: 'home',
        component: HomeComponent
    }, {
        path: 'methods',
        component: MethodsComponent
    }, {
        path: 'method/:id',
        component: MethodDetailComponent
    }, {
        path: 'status',
        component: StatusComponent
    }, {
        path: '**',
        redirectTo: 'home',
        pathMatch: 'full'
    }
];

@NgModule({
    imports: [RouterModule.forRoot(routes)],
    exports: [RouterModule]
})
export class AppRoutingModule {
}
