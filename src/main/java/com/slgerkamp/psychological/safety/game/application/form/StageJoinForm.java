package com.slgerkamp.psychological.safety.game.application.form;

import lombok.*;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;

@NoArgsConstructor
@AllArgsConstructor
public class StageJoinForm {

    @NotNull
    @Length(min=6,max=6)
    private String inputNumber;

    public String getInputNumber() {
        return inputNumber;
    }

    public void setInputNumber(String inputNumber) {
        this.inputNumber = inputNumber;
    }
}
