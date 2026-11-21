package lol.silencecute.client.screen.fatality;

import lol.silencecute.modules.Module;

final class FaEnableRow extends FaCheckRow {

    FaEnableRow(Module module) {

        super(null, "启用",
                () -> !module.toggleable || module.state,
                enabled -> {
                    if (module.toggleable) {
                        module.setState(enabled);
                    } else {
                        module.state = true;
                    }
                });
    }
}
