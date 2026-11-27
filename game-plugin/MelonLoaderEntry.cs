using MelonLoader;
using Redux.GamePlugin;

[assembly: MelonInfo(typeof(ReduxEntry), "REDUX In-Game Menu", "0.1.0", "REDUX")]

public sealed class ReduxEntry : MelonMod
{
    public override void OnInitializeMelon()
    {
        ReduxInGameMenuBehaviour.Install();
    }
}
